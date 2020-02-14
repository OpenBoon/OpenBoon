import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import WarningSvg from '../Icons/warning.svg'

const SIZE = 40

const FormError = ({ children }) => {
  return (
    <div
      css={{
        display: 'flex',
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
      }}>
      <div
        css={{
          display: 'flex',
          justifySelf: 'flex-start',
          color: colors.structure.black,
          fontWeight: typography.weight.medium,
          backgroundColor: colors.structure.white,
          border: constants.borders.error,
          boxShadow: constants.boxShadows.default,
        }}>
        <div
          css={{
            width: SIZE,
            height: SIZE,
            color: colors.structure.white,
            backgroundColor: colors.signal.warning.base,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
          <WarningSvg width={SIZE / 2} />
        </div>
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            paddingLeft: spacing.normal,
            paddingRight: spacing.normal,
          }}>
          {children}
        </div>
      </div>
    </div>
  )
}

FormError.propTypes = {
  children: PropTypes.string.isRequired,
}

export default FormError
