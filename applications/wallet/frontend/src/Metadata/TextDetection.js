import PropTypes from 'prop-types'
import useClipboard from 'react-use-clipboard'

import { colors, constants, spacing } from '../Styles'

import CopySvg from '../Icons/copy.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

const COPY_SIZE = 20

const MetadataTextDetection = ({ name, content }) => {
  const [isCopied, setCopied] = useClipboard(content, { successDuration: 1000 })

  return (
    <>
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.largeDivider,
          },
          padding: spacing.normal,
          paddingBottom: spacing.base,
          fontFamily: 'Roboto Mono',
          color: colors.structure.white,
        }}
      >
        {name}
      </div>
      <div
        css={{
          padding: spacing.normal,
          paddingTop: spacing.base,
          ':hover': {
            backgroundColor: colors.signal.electricBlue.background,
            div: {
              svg: {
                display: 'inline-block',
              },
            },
          },
        }}
      >
        <div
          css={{
            fontFamily: 'Roboto Mono',
            color: colors.structure.white,
            paddingBottom: spacing.base,
            display: 'flex',
          }}
        >
          <div
            css={{
              minHeight: COPY_SIZE,
              width: '100%',
              fontFamily: 'Roboto Condensed',
              textTransform: 'uppercase',
              color: colors.structure.steel,
            }}
          >
            simhash
          </div>
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
        </div>
        <div css={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {content}
        </div>
      </div>
    </>
  )
}

MetadataTextDetection.propTypes = {
  name: PropTypes.string.isRequired,
  content: PropTypes.string.isRequired,
}

export default MetadataTextDetection
