import PropTypes from 'prop-types'
import useClipboard from 'react-use-clipboard'

import { colors } from '../Styles'

import CopySvg from '../Icons/copy.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '.'

export const COPY_SIZE = 20

const ButtonCopy = ({ value }) => {
  const [isCopied, setCopied] = useClipboard(value, { successDuration: 1000 })

  return (
    <Button
      title="Copy to Clipboard"
      variant={BUTTON_VARIANTS.NEUTRAL}
      onClick={setCopied}
      isDisabled={isCopied}
      css={{ ':focus': { svg: { opacity: 1, color: colors.structure.white } } }}
    >
      <CopySvg
        width={COPY_SIZE}
        color={colors.structure.steel}
        css={{
          opacity: 0,
          ':hover': { color: colors.structure.white },
        }}
      />
    </Button>
  )
}

ButtonCopy.propTypes = {
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
}

export default ButtonCopy
