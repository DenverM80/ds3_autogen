        private static ${enum}? ParseNullable${enum}(string ${enum?uncap_first}OrNull)
        {
            return string.IsNullOrWhiteSpace(${enum?uncap_first}OrNull)
                ? (${enum}?) null
                : Parse${enum}(${enum?uncap_first}OrNull);
        }

        private static ${enum} Parse${enum}(string ${enum?uncap_first})
        {
            return ParseEnumType<${enum}>(${enum?uncap_first});
        }

        private static ${enum}? ParseNullable${enum}(XElement element)
        {
            return ParseNullable${enum}(element.Value);
        }

        private static ${enum} Parse${enum}(XElement element)
        {
            return Parse${enum}(element.Value);
        }