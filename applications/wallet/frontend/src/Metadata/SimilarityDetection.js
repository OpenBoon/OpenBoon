import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'

const MetadataSimilarityDetection = ({ name, simhash }) => {
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
        <div css={{ display: 'flex', paddingBottom: spacing.base }}>
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
          <ButtonCopy value={simhash} />
        </div>
        <div
          css={{
            fontFamily: 'Roboto Mono',
            color: colors.structure.white,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
          title={simhash}
        >
          {simhash}
        </div>
      </div>
    </>
  )
}

MetadataSimilarityDetection.propTypes = {
  name: PropTypes.string.isRequired,
  simhash: PropTypes.string.isRequired,
}

export default MetadataSimilarityDetection
