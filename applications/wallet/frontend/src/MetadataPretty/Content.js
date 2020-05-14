import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'

const MetadataPrettyContent = ({ name, value: { content } }) => {
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
      {content && (
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
          content
        </div>
      )}
      <div css={{ paddingBottom: spacing.base }}>
        <div
          css={{
            display: 'flex',
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
          <div
            css={{
              width: '100%',
              padding: `${spacing.moderate}px ${spacing.normal}px`,
              wordBreak: 'break-word',
              color: colors.structure.zinc,
              fontStyle: content ? '' : typography.style.italic,
            }}
          >
            {content || 'No Results'}
          </div>
          <div
            css={{
              minWidth: COPY_SIZE + spacing.normal,
              paddingTop: spacing.moderate,
              paddingRight: spacing.normal,
            }}
          >
            <ButtonCopy value={content} />
          </div>
        </div>
      </div>
    </>
  )
}

MetadataPrettyContent.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    content: PropTypes.string.isRequired,
  }).isRequired,
}

export default MetadataPrettyContent
