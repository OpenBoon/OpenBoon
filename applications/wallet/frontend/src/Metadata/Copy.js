import PropTypes from 'prop-types'

import { colors } from '../Styles'

import CopySvg from '../Icons/copy.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

export const COPY_SIZE = 20

const MetadataCopy = ({ isCopied, setCopied }) => {
  return (
    <Button
      title="Copy to Clipboard"
      variant={BUTTON_VARIANTS.NEUTRAL}
      onClick={setCopied}
      isDisabled={isCopied}
    >
      <CopySvg
        width={COPY_SIZE}
        color={colors.structure.steel}
        css={{
          display: 'none',
          ':hover': { color: colors.structure.white },
        }}
      />
    </Button>
  )
}

MetadataCopy.propTypes = {
  isCopied: PropTypes.bool.isRequired,
  setCopied: PropTypes.func.isRequired,
}

export default MetadataCopy
