import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import CheckboxTriStateIcon, {
  CHECKED,
  UNCHECKED,
  PARTIALLY_CHECKED,
  SIZE,
} from './TriStateIcon'

const CheckboxTriState = ({ status }) => {
  return (
    <button
      type="button"
      css={{
        display: 'flex',
        width: SIZE,
        height: SIZE,
        backgroundColor:
          status === 'UNCHECKED' ? colors.transparent : colors.key.one,
        border:
          status === 'UNCHECKED'
            ? `2px solid ${colors.structure.steel}`
            : `2px solid ${colors.key.one}`,
        borderRadius: constants.borderRadius.small,
        cursor: 'pointer',
        padding: 0,
      }}
      onClick={() => {
        console.warn(status)
      }}>
      <CheckboxTriStateIcon status={status} />
    </button>
  )
}

CheckboxTriState.propTypes = {
  status: PropTypes.oneOf([CHECKED, UNCHECKED, PARTIALLY_CHECKED]).isRequired,
}

export default CheckboxTriState
