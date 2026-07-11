// port-lint: source de/struct_.rs
package io.github.kotlinmania.serderive

import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.quote.appendAll
import io.github.kotlinmania.serderive.checkedQuote
import io.github.kotlinmania.serderive.checkedQuoteSpanned
import io.github.kotlinmania.serderive.internals.AttrContainer
import io.github.kotlinmania.serderive.internals.Default
import io.github.kotlinmania.serderive.internals.Expr
import io.github.kotlinmania.serderive.internals.Field
import io.github.kotlinmania.serderive.internals.Fragment
import io.github.kotlinmania.serderive.internals.Match
import io.github.kotlinmania.serderive.internals.Stmts
import io.github.kotlinmania.syn.Member
import io.github.kotlinmania.syn.span

// Generates the deserialize body for a struct with named fields.
internal fun deserializeStruct(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer,
    form: StructForm
): Fragment {
    val thisType = params.thisType
    val thisValue = params.thisValue
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) =
        params.genericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()

    // If there are getters (implying private fields), construct the local type
    // and use an Into conversion to get the remote type. If there are no
    // getters then construct the target type directly.
    val construct = if (params.hasGetter) {
        val local = params.local
        checkedQuote("`#`local", "local" to local)
    } else {
        checkedQuote("`#`thisValue", "thisValue" to thisValue)
    }

    val typePath = when (form) {
        StructForm.Struct -> construct
        is StructForm.ExternallyTagged -> {
            val variantIdent = form.variantIdent
            checkedQuote("`#`construct::`#`variantIdent",
                "construct" to construct,
                "variantIdent" to variantIdent)
        }
        is StructForm.InternallyTagged -> {
            val variantIdent = form.variantIdent
            checkedQuote("`#`construct::`#`variantIdent",
                "construct" to construct,
                "variantIdent" to variantIdent)
        }
        is StructForm.Untagged -> {
            val variantIdent = form.variantIdent
            checkedQuote("`#`construct::`#`variantIdent",
                "construct" to construct,
                "variantIdent" to variantIdent)
        }
    }
    val expecting = when (form) {
        StructForm.Struct -> "struct ${params.typeName()}"
        is StructForm.ExternallyTagged -> "struct variant ${params.typeName()}::${form.variantIdent}"
        is StructForm.InternallyTagged -> "struct variant ${params.typeName()}::${form.variantIdent}"
        is StructForm.Untagged -> "struct variant ${params.typeName()}::${form.variantIdent}"
    }
    val expectingVal = cattrs.expecting() ?: expecting

    // Skip fields that shouldn't be deserialized or that were flattened,
    // so they don't appear in the storage in their literal form
    val deserializedFields = fields.mapIndexedNotNull { i, field ->
        if (field.attrs.skipDeserializing() || field.attrs.flatten()) return@mapIndexedNotNull null
        FieldWithAliases(
            ident = fieldI(i),
            aliases = field.attrs.aliases()
        )
    }

    val hasFlatten = hasFlatten(fields)
    val fieldVisitor = Stmts(deserializeFieldIdentifier(deserializedFields, cattrs, hasFlatten))

    // Untagged struct variants do not get a visitSeq method. The same applies to
    // structs that only have a map representation.
    val visitSeq = when (form) {
        is StructForm.Untagged -> null
        else -> if (hasFlatten) null else {
            val mutSeq = if (deserializedFields.isEmpty()) {
                checkedQuote("_")
            } else {
                checkedQuote("mut __seq")
            }

            val visitSeqBody = Stmts(deserializeSeq(
                typePath, params, fields, true, cattrs, expectingVal
            ))

            checkedQuote("""
                `#`[inline]
                fn visit_seq<__A>(self, `#`mutSeq: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
                where
                    __A: _serde::de::SeqAccess<`#`delife>,
                {
                    `#`visitSeqBody
                }
            """,
                "Private" to Private,
                "mutSeq" to mutSeq,
                "delife" to delife,
                "visitSeqBody" to visitSeqBody)
        }
    }

    val visitMap = Stmts(deserializeMap(
        typePath,
        params,
        fields,
        cattrs,
        hasFlatten,
    ))

    val visitorSeed = when (form) {
        is StructForm.ExternallyTagged -> if (hasFlatten) {
            checkedQuote("""
                `#`[automatically_derived]
                impl `#`deImplGenerics _serde::de::DeserializeSeed<`#`delife> for __Visitor `#`deTyGenerics `#`whereClause {
                    type Value = `#`thisType `#`tyGenerics;

                    fn deserialize<__D>(self, __deserializer: __D) -> _serde::`#`Private::Result<Self::Value, __D::Error>
                    where
                        __D: _serde::Deserializer<`#`delife>,
                    {
                        _serde::Deserializer::deserialize_map(__deserializer, self)
                    }
                }
            """,
                "deImplGenerics" to deImplGenerics,
                "deTyGenerics" to deTyGenerics,
                "whereClause" to whereClause,
                "delife" to delife,
                "thisType" to thisType,
                "tyGenerics" to tyGenerics,
                "Private" to Private)
        } else null
        else -> null
    }

    val fieldsStmt = if (hasFlatten) {
        null
    } else {
        val fieldNames = deserializedFields.flatMap { field -> field.aliases.toList() }

        checkedQuote("""
            `#`[doc(hidden)]
            const FIELDS: &'static [&'static str] = &[ `#`(`#`fieldNames),* ];
        """,
            "fieldNames" to fieldNames)
    }

    val visitorExpr = checkedQuote("""
        __Visitor {
            marker: _serde::`#`Private::PhantomData::<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData,
        }
    """,
        "Private" to Private,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics)

    val dispatch = when (form) {
        StructForm.Struct -> if (hasFlatten) {
            checkedQuote("""
                _serde::Deserializer::deserialize_map(__deserializer, `#`visitorExpr)
            """, "visitorExpr" to visitorExpr)
        } else {
            val typeName = cattrs.name().deserializeName()
            checkedQuote("""
                _serde::Deserializer::deserialize_struct(__deserializer, `#`typeName, FIELDS, `#`visitorExpr)
            """,
                "typeName" to typeName,
                "visitorExpr" to visitorExpr)
        }
        is StructForm.ExternallyTagged -> if (hasFlatten) {
            checkedQuote("""
                _serde::de::VariantAccess::newtype_variant_seed(__variant, `#`visitorExpr)
            """, "visitorExpr" to visitorExpr)
        } else {
            checkedQuote("""
                _serde::de::VariantAccess::struct_variant(__variant, FIELDS, `#`visitorExpr)
            """, "visitorExpr" to visitorExpr)
        }
        is StructForm.InternallyTagged -> checkedQuote("""
            _serde::Deserializer::deserialize_any(__deserializer, `#`visitorExpr)
        """, "visitorExpr" to visitorExpr)
        is StructForm.Untagged -> checkedQuote("""
            _serde::Deserializer::deserialize_any(__deserializer, `#`visitorExpr)
        """, "visitorExpr" to visitorExpr)
    }

    return Fragment.Block(checkedQuote("""
        `#`fieldVisitor

        `#`[doc(hidden)]
        struct __Visitor `#`deImplGenerics `#`whereClause {
            marker: _serde::`#`Private::PhantomData<`#`thisType `#`tyGenerics>,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`deImplGenerics _serde::de::Visitor<`#`delife> for __Visitor `#`deTyGenerics `#`whereClause {
            type Value = `#`thisType `#`tyGenerics;

            fn expecting(&self, __formatter: &mut _serde::`#`Private::Formatter) -> _serde::`#`Private::fmt::Result {
                _serde::`#`Private::Formatter::write_str(__formatter, `#`expectingVal)
            }

            `#`visitSeq

            `#`[inline]
            fn visit_map<__A>(self, mut __map: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::MapAccess<`#`delife>,
            {
                `#`visitMap
            }
        }

        `#`visitorSeed

        `#`fieldsStmt

        `#`dispatch
    """,
        "fieldVisitor" to fieldVisitor,
        "deImplGenerics" to deImplGenerics,
        "whereClause" to whereClause,
        "Private" to Private,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics,
        "delife" to delife,
        "deTyGenerics" to deTyGenerics,
        "expectingVal" to expectingVal,
        "visitSeq" to visitSeq,
        "visitMap" to visitMap,
        "visitorSeed" to visitorSeed,
        "fieldsStmt" to fieldsStmt,
        "dispatch" to dispatch))
}

private fun deserializeMap(
    structPath: TokenStream,
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer,
    hasFlatten: Boolean
): Fragment {
    // Create the field names for the fields.
    val fieldsNames = fields.mapIndexed { i, field ->
        Pair(field, fieldI(i))
    }

    // Declare each field that will be deserialized.
    val letValues = fieldsNames
        .filter { (field, _) -> !field.attrs.skipDeserializing() && !field.attrs.flatten() }
        .map { (field, name) ->
            val fieldTy = field.ty
            checkedQuote("""
                let mut `#`name: _serde::`#`Private::Option<`#`fieldTy> = _serde::`#`Private::None;
            """,
                "name" to name,
                "Private" to Private,
                "fieldTy" to fieldTy)
        }

    // Collect contents for flatten fields into a buffer
    val letCollect = if (hasFlatten) {
        checkedQuote("""
            let mut __collect = _serde::`#`Private::Vec::<_serde::`#`Private::Option<(
                _serde::`#`Private::de::Content,
                _serde::`#`Private::de::Content
            )>>::new();
        """, "Private" to Private)
    } else {
        checkedQuote("")
    }

    // Match arms to extract a value for a field.
    val valueArms = fieldsNames
        .filter { (field, _) -> !field.attrs.skipDeserializing() && !field.attrs.flatten() }
        .map { (field, name) ->
            val deserName = field.attrs.name().deserializeName()

            val visit = when (val path = field.attrs.deserializeWith()) {
                null -> {
                    val fieldTy = field.ty
                    val span = field.original.span()
                    val func = checkedQuoteSpanned(span, "_serde::de::MapAccess::next_value::<`#`fieldTy>",
                        "fieldTy" to fieldTy)
                    checkedQuote("`#`func(&mut __map)?", "func" to func)
                }
                else -> {
                    val (wrapper, wrapperTy) = wrapDeserializeFieldWith(params, field.ty, path)
                    checkedQuote("""{
                        `#`wrapper
                        match _serde::de::MapAccess::next_value::<`#`wrapperTy>(&mut __map) {
                            _serde::`#`Private::Ok(__wrapper) => __wrapper.value,
                            _serde::`#`Private::Err(__err) => {
                                return _serde::`#`Private::Err(__err);
                            }
                        }
                    }""",
                        "wrapper" to wrapper,
                        "wrapperTy" to wrapperTy,
                        "Private" to Private)
                }
            }

            checkedQuote("""
                __Field::`#`name => {
                    if _serde::`#`Private::Option::is_some(&`#`name) {
                        return _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`deserName));
                    }
                    `#`name = _serde::`#`Private::Some(`#`visit);
                }
            """,
                "name" to name,
                "Private" to Private,
                "deserName" to deserName,
                "visit" to visit)
        }

    // Visit ignored values to consume them
    val ignoredArm = if (hasFlatten) {
        checkedQuote("""
            __Field::__other(__name) => {
                __collect.push(_serde::`#`Private::Some((
                    __name,
                    _serde::de::MapAccess::next_value_seed(&mut __map, _serde::`#`Private::de::ContentVisitor::new())?)));
            }
        """, "Private" to Private)
    } else if (cattrs.denyUnknownFields()) {
        checkedQuote("")
    } else {
        checkedQuote("""
            _ => { let _ = _serde::de::MapAccess::next_value::<_serde::de::IgnoredAny>(&mut __map)?; }
        """)
    }

    val allSkipped = fields.all { it.attrs.skipDeserializing() }
    val matchKeys = if (cattrs.denyUnknownFields() && allSkipped) {
        checkedQuote("""
            // FIXME: Once feature(exhaustive_patterns) is stable:
            // let _serde::`#`Private::None::<__Field> = _serde::de::MapAccess::next_key(&mut __map)?;
            _serde::`#`Private::Option::map(
                _serde::de::MapAccess::next_key::<__Field>(&mut __map)?,
                |__impossible| match __impossible {});
        """, "Private" to Private)
    } else {
        checkedQuote("""
            while let _serde::`#`Private::Some(__key) = _serde::de::MapAccess::next_key::<__Field>(&mut __map)? {
                match __key {
                    `#`(`#`valueArms)*
                    `#`ignoredArm
                }
            }
        """,
            "Private" to Private,
            "valueArms" to valueArms,
            "ignoredArm" to ignoredArm)
    }

    val extractValues = fieldsNames
        .filter { (field, _) -> !field.attrs.skipDeserializing() && !field.attrs.flatten() }
        .map { (field, name) ->
            val missingExpr = Match(exprIsMissing(field, cattrs))

            checkedQuote("""
                let `#`name = match `#`name {
                    _serde::`#`Private::Some(`#`name) => `#`name,
                    _serde::`#`Private::None => `#`missingExpr
                };
            """,
                "name" to name,
                "Private" to Private,
                "missingExpr" to missingExpr)
        }

    val extractCollected = fieldsNames
        .filter { (field, _) -> field.attrs.flatten() && !field.attrs.skipDeserializing() }
        .map { (field, name) ->
            val fieldTy = field.ty
            val func = when (val path = field.attrs.deserializeWith()) {
                null -> {
                    val span = field.original.span()
                    checkedQuoteSpanned(span, "_serde::de::Deserialize::deserialize")
                }
                else -> checkedQuote("`#`path", "path" to path)
            }

            checkedQuote("""
                let `#`name: `#`fieldTy = `#`func(
                    _serde::`#`Private::de::FlatMapDeserializer(
                        &mut __collect,
                        _serde::`#`Private::PhantomData))?;
            """,
                "name" to name,
                "fieldTy" to fieldTy,
                "func" to func,
                "Private" to Private)
        }

    val collectedDenyUnknownFields = if (hasFlatten && cattrs.denyUnknownFields()) {
        checkedQuote("""
            if let _serde::`#`Private::Some(_serde::`#`Private::Some((__key, _))) =
                __collect.into_iter().filter(_serde::`#`Private::Option::is_some).next()
            {
                if let _serde::`#`Private::Some(__key) = _serde::`#`Private::de::content_as_str(&__key) {
                    return _serde::`#`Private::Err(
                        _serde::de::Error::custom(format_args!("unknown field `{}`", &__key)));
                } else {
                    return _serde::`#`Private::Err(
                        _serde::de::Error::custom(format_args!("unexpected map key")));
                }
            }
        """, "Private" to Private)
    } else {
        checkedQuote("")
    }

    val result = fieldsNames.map { (field, name) ->
        val member = field.member
        if (field.attrs.skipDeserializing()) {
            val value = Expr(exprIsMissing(field, cattrs))
            checkedQuote("`#`member: `#`value",
                "member" to member,
                "value" to value)
        } else {
            checkedQuote("`#`member: `#`name",
                "member" to member,
                "name" to name)
        }
    }

    val letDefault = when (cattrs.default()) {
        is Default.Plain -> checkedQuote("let __default: Self::Value = _serde::`#`Private::Default::default();",
            "Private" to Private)
        // If the path returns the wrong type, the error will be reported here.
        // We attach the span of the path to the function so it will be reported
        // on the serde default attribute.
        is Default.Path -> {
            val p = (cattrs.default() as Default.Path).path
            checkedQuoteSpanned(p.span(), "let __default: Self::Value = `#`p();",
                "p" to p)
        }
        is Default.None -> {
            // We don't need the default value, to prevent an unused variable warning
            // we'll leave the line empty.
            checkedQuote("")
        }
    }

    var resultExpr = checkedQuote("`#`structPath { `#`(`#`result),* }",
        "structPath" to structPath,
        "result" to result)
    if (params.hasGetter) {
        val thisType = params.thisType
        val split = params.generics.splitForImpl()
        val tyGenerics = split.typeGenerics
        resultExpr = checkedQuote("""
            _serde::`#`Private::Into::<`#`thisType `#`tyGenerics>::into(`#`resultExpr)
        """,
            "Private" to Private,
            "thisType" to thisType,
            "tyGenerics" to tyGenerics,
            "resultExpr" to resultExpr)
    }

    return Fragment.Block(checkedQuote("""
        `#`(`#`letValues)*

        `#`letCollect

        `#`matchKeys

        `#`letDefault

        `#`(`#`extractValues)*

        `#`(`#`extractCollected)*

        `#`collectedDenyUnknownFields

        _serde::`#`Private::Ok(`#`resultExpr)
    """,
        "letValues" to letValues,
        "letCollect" to letCollect,
        "matchKeys" to matchKeys,
        "letDefault" to letDefault,
        "extractValues" to extractValues,
        "extractCollected" to extractCollected,
        "collectedDenyUnknownFields" to collectedDenyUnknownFields,
        "Private" to Private,
        "resultExpr" to resultExpr))
}

// Generates the deserialize-in-place body for a struct with named fields.
internal fun deserializeStructInPlace(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer
): Fragment? {
    // We do not support in-place deserialization for structs that
    // are represented as a map.
    if (hasFlatten(fields)) {
        return null
    }

    val thisType = params.thisType
    val (deImplGenerics, deTyGenerics, tyGenerics, whereClause) =
        params.inPlaceGenericsWithDeLifetime()
    val delife = params.borrowed.deLifetime()

    val expecting = "struct ${params.typeName()}"
    val expectingVal = cattrs.expecting() ?: expecting

    val deserializedFields = fields.mapIndexedNotNull { i, field ->
        if (field.attrs.skipDeserializing()) return@mapIndexedNotNull null
        FieldWithAliases(
            ident = fieldI(i),
            aliases = field.attrs.aliases()
        )
    }

    val fieldVisitor = Stmts(deserializeFieldIdentifier(deserializedFields, cattrs, false))

    val mutSeq = if (deserializedFields.isEmpty()) {
        checkedQuote("_")
    } else {
        checkedQuote("mut __seq")
    }
    val visitSeq = Stmts(deserializeSeqInPlace(params, fields, cattrs, expectingVal))
    val visitMap = Stmts(deserializeMapInPlace(params, fields, cattrs))
    val fieldNames = deserializedFields.flatMap { field -> field.aliases.toList() }
    val typeName = cattrs.name().deserializeName()

    val inPlaceImplGenerics = InPlaceImplGenerics(params)
    val inPlaceTyGenerics = InPlaceTypeGenerics(params)
    val placeLife = placeLifetime()

    return Fragment.Block(checkedQuote("""
        `#`fieldVisitor

        `#`[doc(hidden)]
        struct __Visitor `#`inPlaceImplGenerics `#`whereClause {
            place: &`#`placeLife mut `#`thisType `#`tyGenerics,
            lifetime: _serde::`#`Private::PhantomData<&`#`delife ()>,
        }

        `#`[automatically_derived]
        impl `#`inPlaceImplGenerics _serde::de::Visitor<`#`delife> for __Visitor `#`inPlaceTyGenerics `#`whereClause {
            type Value = ();

            fn expecting(&self, __formatter: &mut _serde::`#`Private::Formatter) -> _serde::`#`Private::fmt::Result {
                _serde::`#`Private::Formatter::write_str(__formatter, `#`expectingVal)
            }

            `#`[inline]
            fn visit_seq<__A>(self, `#`mutSeq: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::SeqAccess<`#`delife>,
            {
                `#`visitSeq
            }

            `#`[inline]
            fn visit_map<__A>(self, mut __map: __A) -> _serde::`#`Private::Result<Self::Value, __A::Error>
            where
                __A: _serde::de::MapAccess<`#`delife>,
            {
                `#`visitMap
            }
        }

        `#`[doc(hidden)]
        const FIELDS: &'static [&'static str] = &[ `#`(`#`fieldNames),* ];

        _serde::Deserializer::deserialize_struct(__deserializer, `#`typeName, FIELDS, __Visitor {
            place: __place,
            lifetime: _serde::`#`Private::PhantomData,
        })
    """,
        "fieldVisitor" to fieldVisitor,
        "inPlaceImplGenerics" to inPlaceImplGenerics,
        "whereClause" to whereClause,
        "placeLife" to placeLife,
        "thisType" to thisType,
        "tyGenerics" to tyGenerics,
        "Private" to Private,
        "delife" to delife,
        "inPlaceTyGenerics" to inPlaceTyGenerics,
        "expectingVal" to expectingVal,
        "mutSeq" to mutSeq,
        "visitSeq" to visitSeq,
        "visitMap" to visitMap,
        "fieldNames" to fieldNames,
        "typeName" to typeName))
}

private fun deserializeMapInPlace(
    params: Parameters,
    fields: List<Field>,
    cattrs: AttrContainer
): Fragment {
    check(!hasFlatten(fields)) { "inplace deserialization of maps does not support flatten fields" }

    // Create the field names for the fields.
    val fieldsNames = fields.mapIndexed { i, field ->
        Pair(field, fieldI(i))
    }

    // For in-place deserialization, declare booleans for each field that will be
    // deserialized.
    val letFlags = fieldsNames
        .filter { (field, _) -> !field.attrs.skipDeserializing() }
        .map { (_, name) ->
            checkedQuote("""
                let mut `#`name: bool = false;
            """, "name" to name)
        }

    // Match arms to extract a value for a field.
    val valueArmsFrom = fieldsNames
        .filter { (field, _) -> !field.attrs.skipDeserializing() }
        .map { (field, name) ->
            val deserName = field.attrs.name().deserializeName()
            val member = field.member

            val visit = when (val path = field.attrs.deserializeWith()) {
                null -> {
                    checkedQuote("""
                        _serde::de::MapAccess::next_value_seed(&mut __map, _serde::`#`Private::de::InPlaceSeed(&mut self.place.`#`member))?
                    """,
                        "Private" to Private,
                        "member" to member)
                }
                else -> {
                    val (wrapper, wrapperTy) = wrapDeserializeFieldWith(params, field.ty, path)
                    checkedQuote("""{
                        `#`wrapper
                        self.place.`#`member = match _serde::de::MapAccess::next_value::<`#`wrapperTy>(&mut __map) {
                            _serde::`#`Private::Ok(__wrapper) => __wrapper.value,
                            _serde::`#`Private::Err(__err) => {
                                return _serde::`#`Private::Err(__err);
                            }
                        };
                    }""",
                        "wrapper" to wrapper,
                        "member" to member,
                        "wrapperTy" to wrapperTy,
                        "Private" to Private)
                }
            }

            checkedQuote("""
                __Field::`#`name => {
                    if `#`name {
                        return _serde::`#`Private::Err(<__A::Error as _serde::de::Error>::duplicate_field(`#`deserName));
                    }
                    `#`visit;
                    `#`name = true;
                }
            """,
                "name" to name,
                "Private" to Private,
                "deserName" to deserName,
                "visit" to visit)
        }

    // Visit ignored values to consume them
    val ignoredArm = if (cattrs.denyUnknownFields()) {
        checkedQuote("")
    } else {
        checkedQuote("""
            _ => { let _ = _serde::de::MapAccess::next_value::<_serde::de::IgnoredAny>(&mut __map)?; }
        """)
    }

    val allSkipped = fields.all { it.attrs.skipDeserializing() }

    val matchKeys = if (cattrs.denyUnknownFields() && allSkipped) {
        checkedQuote("""
            // FIXME: Once feature(exhaustive_patterns) is stable:
            // let _serde::`#`Private::None::<__Field> = _serde::de::MapAccess::next_key(&mut __map)?;
            _serde::`#`Private::Option::map(
                _serde::de::MapAccess::next_key::<__Field>(&mut __map)?,
                |__impossible| match __impossible {});
        """, "Private" to Private)
    } else {
        checkedQuote("""
            while let _serde::`#`Private::Some(__key) = _serde::de::MapAccess::next_key::<__Field>(&mut __map)? {
                match __key {
                    `#`(`#`valueArmsFrom)*
                    `#`ignoredArm
                }
            }
        """,
            "Private" to Private,
            "valueArmsFrom" to valueArmsFrom,
            "ignoredArm" to ignoredArm)
    }

    val checkFlags = fieldsNames
        .filter { (field, _) -> !field.attrs.skipDeserializing() }
        .map { (field, name) ->
            val missingExpr = exprIsMissing(field, cattrs)
            // If the missing expression unconditionally returns an error, don't try
            // to assign its value to the in-place target.
            if (field.attrs.default() is Default.None
                && cattrs.default() is Default.None
                && field.attrs.deserializeWith() != null
            ) {
                val missingExprStmts = Stmts(missingExpr)
                checkedQuote("""
                    if !`#`name {
                        `#`missingExprStmts;
                    }
                """,
                    "name" to name,
                    "missingExprStmts" to missingExprStmts)
            } else {
                val member = field.member
                val missingExprExpr = Expr(missingExpr)
                checkedQuote("""
                    if !`#`name {
                        self.place.`#`member = `#`missingExprExpr;
                    };
                """,
                    "name" to name,
                    "member" to member,
                    "missingExprExpr" to missingExprExpr)
            }
        }

    val thisType = params.thisType
    val split = params.generics.splitForImpl()
    val tyGenerics = split.typeGenerics

    val letDefault = when (cattrs.default()) {
        is Default.Plain -> checkedQuote("let __default: `#`thisType `#`tyGenerics = _serde::`#`Private::Default::default();",
            "thisType" to thisType,
            "tyGenerics" to tyGenerics,
            "Private" to Private)
        // If the path returns the wrong type, the error will be reported here.
        // We attach the span of the path to the function so it will be reported
        // on the serde default attribute.
        is Default.Path -> {
            val p = (cattrs.default() as Default.Path).path
            checkedQuoteSpanned(p.span(), "let __default: `#`thisType `#`tyGenerics = `#`p();",
                "thisType" to thisType,
                "tyGenerics" to tyGenerics,
                "p" to p)
        }
        is Default.None -> {
            // We don't need the default value, to prevent an unused variable warning
            // we'll leave the line empty.
            checkedQuote("")
        }
    }

    return Fragment.Block(checkedQuote("""
        `#`(`#`letFlags)*

        `#`matchKeys

        `#`letDefault

        `#`(`#`checkFlags)*

        _serde::`#`Private::Ok(())
    """,
        "letFlags" to letFlags,
        "matchKeys" to matchKeys,
        "letDefault" to letDefault,
        "checkFlags" to checkFlags,
        "Private" to Private))
}

// Generates the enum and its Deserialize implementation that represents each
// non-skipped field of the struct.
private fun deserializeFieldIdentifier(
    deserializedFields: List<FieldWithAliases>,
    cattrs: AttrContainer,
    hasFlatten: Boolean
): Fragment {
    val (ignoreVariant, fallthrough) = if (hasFlatten) {
        val ignoreVariant = checkedQuote("__other(_serde::`#`Private::de::Content<'de>),", "Private" to Private)
        val fallthrough = checkedQuote("_serde::`#`Private::Ok(__Field::__other(__value))", "Private" to Private)
        Pair(ignoreVariant, fallthrough)
    } else if (cattrs.denyUnknownFields()) {
        Pair(null, null)
    } else {
        val ignoreVariant = checkedQuote("__ignore,")
        val fallthrough = checkedQuote("_serde::`#`Private::Ok(__Field::__ignore)", "Private" to Private)
        Pair(ignoreVariant, fallthrough)
    }

    return deserializeGenerated(
        deserializedFields,
        hasFlatten,
        false,
        ignoreVariant,
        fallthrough,
    )
}
