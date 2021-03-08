import PropTypes from 'prop-types'

import { constants, spacing, colors, typography } from '../Styles'
import stylesShape from '../Styles/shape'

const BASE = {
  width: '100%',
  flex: 1,
  padding: spacing.moderate,
  paddingLeft: spacing.spacious,
  border: constants.borders.regular.transparent,
  borderRadius: constants.borderRadius.small,
  color: colors.structure.pebble,
  backgroundRepeat: `no-repeat, repeat`,
  backgroundPosition: `left ${spacing.base}px top 50%`,
  backgroundSize: constants.icons.regular,
  '::placeholder': {
    fontStyle: typography.style.italic,
  },
  ':hover': {
    border: constants.borders.regular.steel,
  },
  ':focus': {
    outline: constants.borders.regular.transparent,
    border: constants.borders.keyOneRegular,
    color: colors.structure.coal,
    backgroundColor: colors.structure.white,
    backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjYjNiM2IzIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
  },
}

const STYLES = {
  LIGHT: {
    backgroundColor: colors.structure.white,
    backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjYjNiM2IzIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
  },
  DARK: {
    backgroundColor: colors.structure.mattGrey,
    backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjNGE0YTRhIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
  },
  EXTRADARK: {
    paddingLeft: spacing.large,
    backgroundColor: colors.structure.smoke,
    backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+DQogICAgPHBhdGggZmlsbD0iIzgwODA4MCIgZD0iTTEzLjg1NyAxMi4zMTRoLS44MjNsLS4zMDgtLjMwOGE2LjQzOCA2LjQzOCAwIDAwMS42NDUtNC4zMkE2LjY3MiA2LjY3MiAwIDAwNy42ODYgMSA2LjY3MiA2LjY3MiAwIDAwMSA3LjY4NmE2LjY3MiA2LjY3MiAwIDAwNi42ODYgNi42ODUgNi40MzggNi40MzggMCAwMDQuMzItMS42NDVsLjMwOC4zMDh2LjgyM0wxNy40NTcgMTkgMTkgMTcuNDU3bC01LjE0My01LjE0M3ptLTYuMTcxIDBhNC42MSA0LjYxIDAgMDEtNC42MjktNC42MjggNC42MSA0LjYxIDAgMDE0LjYyOS00LjYyOSA0LjYxIDQuNjEgMCAwMTQuNjI4IDQuNjI5IDQuNjEgNC42MSAwIDAxLTQuNjI4IDQuNjI4eiIvPg0KPC9zdmc+')`,
    '::placeholder': {
      fontStyle: typography.style.normal,
      color: colors.structure.zinc,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const InputSearch = ({
  placeholder,
  value,
  onChange,
  variant,
  style,
  ...props
}) => {
  return (
    <input
      type="search"
      value={value}
      placeholder={placeholder}
      onChange={({ target: { value: v } }) => onChange({ value: v })}
      css={[BASE, STYLES[variant], style]}
      // eslint-disable-next-line react/jsx-props-no-spreading
      {...props}
    />
  )
}

InputSearch.defaultProps = {
  style: {},
}

InputSearch.propTypes = {
  placeholder: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  style: stylesShape,
}

export default InputSearch
