import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'

const MetadataPrettySimilarity = ({ name, value: { simhash } }) => {
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
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.white,
        }}
      >
        {name}
      </div>
      <div
        css={{
          padding: `${spacing.base}px ${spacing.normal}px`,
          paddingBottom: 0,
          minHeight: COPY_SIZE,
          width: '100%',
          fontFamily: 'Roboto Condensed',
          textTransform: 'uppercase',
          color: colors.structure.steel,
        }}
      >
        simhash
      </div>
      <div css={{ paddingBottom: spacing.base }}>
        <div
          css={{
            display: 'flex',
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
              padding: `${spacing.moderate}px ${spacing.normal}px`,
              fontFamily: 'Roboto Mono',
              fontSize: typography.size.small,
              lineHeight: typography.height.small,
              color: colors.structure.white,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
            title={simhash}
          >
            {simhash}
          </div>
          <div
            css={{
              minWidth: COPY_SIZE + spacing.normal,
              paddingTop: spacing.moderate,
              paddingRight: spacing.normal,
            }}
          >
            <ButtonCopy value={simhash} />
          </div>
        </div>
      </div>
    </>
  )
}

MetadataPrettySimilarity.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    simhash: PropTypes.string.isRequired,
  }).isRequired,
}

export default MetadataPrettySimilarity
