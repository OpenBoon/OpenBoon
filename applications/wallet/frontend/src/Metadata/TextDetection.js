import PropTypes from 'prop-types'
import useClipboard from 'react-use-clipboard'

import { colors, constants, spacing, typography } from '../Styles'

import MetadataCopy, { COPY_SIZE } from './Copy'

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
          ':hover': content
            ? {
                backgroundColor: colors.signal.electricBlue.background,
                div: {
                  svg: {
                    display: 'inline-block',
                  },
                },
              }
            : {},
        }}
      >
        {content && (
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
              content
            </div>
            <MetadataCopy isCopied={isCopied} setCopied={setCopied} />
          </div>
        )}
        <div
          css={{
            wordBreak: 'break-word',
            color: colors.structure.zinc,
            fontStyle: content ? '' : typography.style.italic,
          }}
        >
          {content || 'No Results'}
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
