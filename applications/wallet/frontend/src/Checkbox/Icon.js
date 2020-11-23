import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

const getBorder = ({ isChecked, isDisabled }) => {
  if (isDisabled && isChecked) return 'none'

  if (isChecked) return `2px solid ${colors.key.one}`

  if (isDisabled) return `2px solid ${colors.structure.mattGrey}`

  return `2px solid ${colors.structure.steel}`
}

const CheckboxIcon = ({
  size,
  value,
  isChecked,
  isDisabled,
  onClick,
  onBlur,
}) => (
  <div css={{ display: 'flex', position: 'relative' }}>
    <input
      type="checkbox"
      value={value}
      defaultChecked={isChecked}
      onClick={onClick}
      onBlur={onBlur}
      css={{
        padding: 0,
        width: size,
        height: size,
        WebkitAppearance: 'none',
        backgroundColor:
          isChecked && !isDisabled
            ? colors.key.one
            : colors.structure.transparent,
        border: getBorder({ isChecked, isDisabled }),
        borderRadius: constants.borderRadius.small,
        cursor: isDisabled ? 'not-allowed' : 'pointer',
      }}
    />
    <div
      css={{
        position: 'absolute',
        top: 0,
        left: 0,
        bottom: 0,
        width: size,
        alignItems: 'center',
        justifyContent: 'center',
        display: 'flex',
        cursor: isDisabled ? 'not-allowed' : 'pointer',
      }}
    >
      <CheckmarkSvg
        height={constants.icons.regular}
        css={{
          path: {
            transition: 'all .3s ease',
            opacity: isChecked ? 100 : 0,
            fill: isDisabled ? colors.key.one : colors.white,
          },
        }}
      />
    </div>
  </div>
)

CheckboxIcon.defaultProps = {
  onBlur: undefined,
}

CheckboxIcon.propTypes = {
  size: PropTypes.number.isRequired,
  value: PropTypes.string.isRequired,
  isChecked: PropTypes.bool.isRequired,
  isDisabled: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired,
  onBlur: PropTypes.func,
}

export default CheckboxIcon
