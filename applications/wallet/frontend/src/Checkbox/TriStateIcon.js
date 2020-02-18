import PropTypes from 'prop-types'
import { colors } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import CheckboxDashSvg from '../Icons/checkboxdash.svg'

export const SIZE = 28
export const VARIANTS = {
  CHECKED: 'CHECKED',
  UNCHECKED: 'UNCHECKED',
  PARTIALLY_CHECKED: 'PARTIALLY_CHECKED',
}

const CheckboxTriStateIcon = ({ status }) => {
  if (status === VARIANTS.CHECKED) {
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
  if (status === VARIANTS.PARTIALLY_CHECKED) {
    return (
      <CheckboxDashSvg
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

CheckboxTriStateIcon.propTypes = {
  status: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
}

export default CheckboxTriStateIcon
