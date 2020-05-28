import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

const RADIO_BUTTON_SIZE = 16
const RADIO_BUTTION_FILL_SIZE = 8

const RadioIcon = ({ value, isChecked }) => {
  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <input
        type="radio"
        id={value}
        value={value}
        defaultChecked={isChecked}
        css={{
          margin: 0,
          padding: 0,
          WebkitAppearance: 'none',
          borderRadius: RADIO_BUTTON_SIZE,
          width: RADIO_BUTTON_SIZE,
          height: RADIO_BUTTON_SIZE,
          border: constants.borders.radio,
        }}
      />
      <div
        css={{
          position: 'absolute',
          width: RADIO_BUTTION_FILL_SIZE,
          height: RADIO_BUTTION_FILL_SIZE,
          transition: 'all .3s ease',
          opacity: 100,
          borderRadius: RADIO_BUTTON_SIZE,
          backgroundColor: isChecked ? colors.key.one : 'none',
        }}
      />
    </div>
  )
}

RadioIcon.propTypes = {
  value: PropTypes.string.isRequired,
  isChecked: PropTypes.bool.isRequired,
}

export default RadioIcon
