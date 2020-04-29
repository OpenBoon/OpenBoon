import PropTypes from 'prop-types'
import useClipboard from 'react-use-clipboard'

import { colors, constants, spacing } from '../Styles'

import MetadataCopy, { COPY_SIZE } from './Copy'

const MetadataImageSimilarity = ({ name, simhash }) => {
  const [isCopied, setCopied] = useClipboard(simhash, { successDuration: 1000 })
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
          <MetadataCopy isCopied={isCopied} setCopied={setCopied} />
        </div>
        <div css={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {simhash}
        </div>
      </div>
    </>
  )
}

MetadataImageSimilarity.propTypes = {
  name: PropTypes.string.isRequired,
  simhash: PropTypes.string.isRequired,
}

export default MetadataImageSimilarity
