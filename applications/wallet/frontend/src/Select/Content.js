import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import { colors, constants, spacing, typography } from '../Styles'

const WIDTH = 300
const HEIGHT = 40

const SelectContent = ({
  label,
  options,
  defaultValue,
  onChange,
  isDisabled,
  useAria,
  style,
}) => {
  const ariaLabel = useAria ? { 'aria-label': label } : {}
  return (
    <select
      // eslint-disable-next-line react/jsx-props-no-spreading
      {...ariaLabel}
      disabled={isDisabled}
      defaultValue={defaultValue}
      onChange={({ target: { value } }) => onChange({ value })}
      css={{
        backgroundColor: colors.structure.steel,
        borderRadius: constants.borderRadius.small,
        border: 'none',
        width: WIDTH,
        height: HEIGHT,
        color: colors.structure.white,
        fontSize: typography.size.regular,
        lineHeight: typography.height.regular,
        paddingLeft: spacing.moderate,
        paddingRight: spacing.spacious,
        MozAppearance: 'none',
        WebkitAppearance: 'none',
        backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2ZmZmZmZiIgLz4KPC9zdmc+')`,
        backgroundRepeat: `no-repeat, repeat`,
        backgroundPosition: `right ${spacing.base}px top 50%`,
        backgroundSize: constants.icons.regular,
        ...style,
      }}
    >
      <option value="" disabled>
        Select an option...
      </option>
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  )
}

SelectContent.propTypes = {
  label: PropTypes.node.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string,
      label: PropTypes.string,
    }),
  ).isRequired,
  defaultValue: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  isDisabled: PropTypes.bool.isRequired,
  useAria: PropTypes.bool.isRequired,
  style: stylesShape.isRequired,
}

export default SelectContent
