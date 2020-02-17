import PropTypes from 'prop-types'
import { colors } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import CheckboxPartialSvg from '../Icons/checkboxpartial.svg'

export const SIZE = 28
export const CHECKED = 'CHECKED'
export const UNCHECKED = 'UNCHECKED'
export const PARTIALLY_CHECKED = 'PARTIALLY_CHECKED'

const TriStateIcon = ({ status }) => {
  if (status === CHECKED) {
    return (
      <CheckmarkSvg
        width={SIZE}
        css={{
          path: {
            transition: 'all .3s ease',
            opacity: 100,
          },
          color: colors.structure.white,
        }}
      />
    )
  }
  if (status === PARTIALLY_CHECKED) {
    return (
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
  }

  return (
    <CheckmarkSvg
      width={SIZE}
      css={{
        path: {
          transition: 'all .3s ease',
          opacity: 0,
        },
      }}
    />
  )
}

TriStateIcon.propTypes = {
  status: PropTypes.oneOf([CHECKED, UNCHECKED, PARTIALLY_CHECKED]).isRequired,
}

export default TriStateIcon
