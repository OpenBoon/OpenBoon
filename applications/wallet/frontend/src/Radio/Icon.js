import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

const RADIO_BUTTON_SIZE = 16
const RADIO_BUTTION_FILL_SIZE = 8

const RadioIcon = ({ name, value, isChecked, onClick }) => {
  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
      }}
    >
      <input
        type="radio"
        name={name}
        value={value}
        defaultChecked={isChecked}
        onClick={() => onClick({ value })}
        css={{
          padding: 0,
          WebkitAppearance: 'none',
          borderRadius: RADIO_BUTTON_SIZE,
          width: RADIO_BUTTON_SIZE,
          height: RADIO_BUTTON_SIZE,
          border: isChecked
            ? constants.borders.regular.white
            : constants.borders.regular.steel,
        }}
      />

      <div
        css={{
          position: 'absolute',
          width: RADIO_BUTTION_FILL_SIZE,
          height: RADIO_BUTTION_FILL_SIZE,
          opacity: 100,
          borderRadius: RADIO_BUTTON_SIZE,
          backgroundColor: isChecked ? colors.key.one : 'none',
        }}
      />
    </div>
  )
}

RadioIcon.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  isChecked: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired,
}

export default RadioIcon
