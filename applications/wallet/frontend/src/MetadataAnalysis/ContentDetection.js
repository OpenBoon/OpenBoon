import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'

const MetadataAnalysisContentDetection = ({ name, value: { content } }) => {
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
              display: 'flex',
              paddingBottom: spacing.base,
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
            <ButtonCopy value={content} />
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

MetadataAnalysisContentDetection.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    content: PropTypes.string.isRequired,
  }).isRequired,
}

export default MetadataAnalysisContentDetection
