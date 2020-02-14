import PropTypes from 'prop-types'
import { useReducer } from 'react'

import { colors, constants } from '../Styles'
import CheckmarkSvg from '../Icons/checkmark.svg'
import CheckboxPartialSvg from '../Icons/checkboxpartial.svg'

const SIZE = 28

const reducer = (state, action) => ({ ...state, ...action })

const CheckBoxTriState = ({ isPartial, isChecked }) => {
  const [state, dispatch] = useReducer(reducer, { isPartial, isChecked })

  const changeState = () => {
    if (state.isChecked) {
      return dispatch({ isPartial: false, isChecked: false })
    }

    if (state.isPartial) {
      return dispatch({ isPartial: false, isChecked: true })
    }

    if (!state.isPartial && !state.isChecked) {
      return dispatch({ isPartial: false, isChecked: true })
    }
  }

  const isPartialIcon = (
    <CheckboxPartialSvg
      width={SIZE}
      css={{
        path: {
          transition: 'all .3s ease',
          opacity: 100,
        },
        color: colors.key.one,
      }}
    />
  )

  const isSelectedIcon = (
    <CheckmarkSvg
      width={SIZE}
      css={{
        path: {
          transition: 'all .3s ease',
          opacity: 100,
          fill: colors.white,
        },
      }}
    />
  )

  const isUnselectedIcon = (
    <CheckmarkSvg
      width={SIZE}
      css={{
        path: {
          transition: 'all .3s ease',
          opacity: 0,
          fill: colors.white,
        },
      }}
    />
  )

  let displayIcon
  if (!state.isPartial && !state.isChecked) {
    displayIcon = isUnselectedIcon
  } else if (state.isPartial) {
    displayIcon = isPartialIcon
  } else {
    displayIcon = isSelectedIcon
  }

  return (
    <div
      role="button"
      tabIndex="0"
      css={{
        display: 'flex',
        width: SIZE,
        height: SIZE,
        backgroundColor:
          state.isPartial || state.isChecked
            ? colors.key.one
            : colors.transparent,
        border:
          state.isPartial || state.isChecked
            ? `2px solid ${colors.key.one}`
            : `2px solid ${colors.structure.steel}`,
        borderRadius: constants.borderRadius.small,
        cursor: 'pointer',
      }}
      onClick={changeState}
      onKeyDown={changeState}>
      {displayIcon}
    </div>
  )
}

CheckBoxTriState.propTypes = {
  isPartial: PropTypes.bool.isRequired,
  isChecked: PropTypes.bool.isRequired,
}

export default CheckBoxTriState
