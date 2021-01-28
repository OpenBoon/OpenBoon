import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import { colors, constants, spacing, typography } from '../Styles'

const WIDTH = 300

export const VARIANTS = {
  ROW: 'ROW',
  COLUMN: 'COLUMN',
}

const Select = ({
  label,
  options,
  defaultValue,
  onChange,
  isRequired,
  isDisabled,
  variant,
  style,
}) => {
  return (
    <label
      css={{
        color: colors.structure.zinc,
        ...(variant === 'ROW' ? { display: 'flex', alignItems: 'center' } : {}),
      }}
    >
      {label}
      {isRequired && (
        <span css={{ color: colors.signal.warning.base }}> *</span>
      )}
      <div
        css={{
          ...(variant === 'ROW'
            ? { paddingLeft: spacing.base }
            : { paddingTop: spacing.base, paddingBottom: spacing.base }),
        }}
      >
        <select
          disabled={isDisabled}
          defaultValue={defaultValue}
          onChange={({ target: { value } }) => onChange({ value })}
          css={{
            backgroundColor: colors.structure.steel,
            borderRadius: constants.borderRadius.small,
            border: 'none',
            width: WIDTH,
            color: colors.structure.white,
            fontSize: typography.size.regular,
            lineHeight: typography.height.medium,
            paddingTop: spacing.moderate,
            paddingBottom: spacing.moderate,
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
      </div>
    </label>
  )
}

Select.defaultProps = {
  defaultValue: '',
  isDisabled: false,
  style: {},
}

Select.propTypes = {
  label: PropTypes.node.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string,
      label: PropTypes.string,
    }),
  ).isRequired,
  defaultValue: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  isRequired: PropTypes.bool.isRequired,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  isDisabled: PropTypes.bool,
  style: stylesShape,
}

export default Select
