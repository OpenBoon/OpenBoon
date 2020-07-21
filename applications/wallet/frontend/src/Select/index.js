import PropTypes from 'prop-types'

import styleShape from '../Style/shape'

import { colors, constants, spacing, typography } from '../Styles'

const WIDTH = 300
const HEIGHT = 40
const ICON_SIZE = 20

const Select = ({ htmlFor, label, placeholder, onChange, children, style }) => {
  return (
    <>
      {label && (
        <label htmlFor={htmlFor} css={{ color: colors.structure.zinc }}>
          {label}
        </label>
      )}
      <div css={{ paddingTop: spacing.base, paddingBottom: spacing.base }}>
        <select
          name={htmlFor}
          id={htmlFor}
          defaultValue=""
          onChange={onChange}
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
            MozAppearance: 'none',
            WebkitAppearance: 'none',
            backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2ZmZmZmZiIgLz4KPC9zdmc+')`,
            backgroundRepeat: `no-repeat, repeat`,
            backgroundPosition: `right ${spacing.base}px top 50%`,
            backgroundSize: ICON_SIZE,
            ...style,
          }}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {children}
        </select>
      </div>
    </>
  )
}

Select.defaultProps = {
  style: {},
}

Select.propTypes = {
  htmlFor: PropTypes.string.isRequired,
  label: PropTypes.node.isRequired,
  placeholder: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
  style: styleShape,
}

export default Select
