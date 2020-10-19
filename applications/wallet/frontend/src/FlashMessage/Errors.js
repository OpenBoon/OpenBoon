import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '.'

// Add the proper styling so the space above and below each total to 24px
const FlashMessageErrors = ({ errors, styles }) => {
  const { global = '' } = errors

  if (!global) return null

  return (
    <div
      css={{
        display: 'flex',
        ...styles,
      }}
    >
      <FlashMessage variant={FLASH_VARIANTS.ERROR}>{global}</FlashMessage>
    </div>
  )
}

FlashMessageErrors.defaultProps = {
  styles: {},
}

FlashMessageErrors.propTypes = {
  errors: PropTypes.shape({ global: PropTypes.string }).isRequired,
  styles: stylesShape,
}

export default FlashMessageErrors
