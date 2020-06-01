import PropTypes from 'prop-types'

import { colors, spacing, constants, typography } from '../Styles'

const ICON_SIZE = 20

const FilterSearch = ({ placeholder, searchString, onChange }) => {
  return (
    <div css={{ paddingBottom: spacing.normal }}>
      <input
        type="search"
        placeholder={placeholder}
        value={searchString}
        onChange={({ target: { value } }) => {
          onChange({ value })
        }}
        css={{
          width: '100%',
          border: constants.borders.transparent,
          padding: spacing.moderate,
          paddingLeft: spacing.spacious,
          borderRadius: constants.borderRadius.small,
          color: colors.structure.pebble,
          backgroundColor: colors.structure.mattGrey,
          ':hover': {
            border: constants.borders.tableRow,
          },
          ':focus': {
            outline: constants.borders.outline,
            border: constants.borders.inputSmall,
            color: colors.structure.coal,
            backgroundColor: colors.structure.white,
            backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjYjNiM2IzIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
          },

          '::placeholder': {
            fontStyle: typography.style.italic,
          },
          backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjNGE0YTRhIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
          backgroundRepeat: `no-repeat, repeat`,
          backgroundPosition: `left ${spacing.base}px top 50%`,
          backgroundSize: ICON_SIZE,
        }}
      />
    </div>
  )
}

FilterSearch.propTypes = {
  placeholder: PropTypes.string.isRequired,
  searchString: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
}

export default FilterSearch
