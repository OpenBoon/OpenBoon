import PropTypes from 'prop-types'
import useClipboard from 'react-use-clipboard'

import CopySvg from '../Icons/copy.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '.'

export const COPY_SIZE = 20

const ButtonCopy = ({ value }) => {
  const [isCopied, setCopied] = useClipboard(value, { successDuration: 1000 })

  return (
    <Button
      title="Copy to Clipboard"
      variant={BUTTON_VARIANTS.ICON}
      onClick={setCopied}
      isDisabled={isCopied}
      css={{
        padding: 0,
        svg: {
          opacity: 0,
        },
      }}
    >
      <CopySvg height={COPY_SIZE} />
    </Button>
  )
}

ButtonCopy.propTypes = {
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
}

export default ButtonCopy
