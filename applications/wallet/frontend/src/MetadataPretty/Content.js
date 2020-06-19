import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'

import MetadataPrettyNoResults from './NoResults'

const MetadataPrettyContent = ({ name, value: { content } }) => {
  if (!content) {
    return <MetadataPrettyNoResults name={name} />
  }

  return (
    <>
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.largeDivider,
          },
          padding: spacing.normal,
          paddingBottom: spacing.base,
          fontFamily: typography.family.mono,
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
          fontFamily: typography.family.condensed,
          textTransform: 'uppercase',
          color: colors.structure.steel,
        }}
      >
        content
      </div>

      <div css={{ paddingBottom: spacing.base }}>
        <div
          css={{
            display: 'flex',
            ':hover': {
              backgroundColor: colors.signal.electricBlue.background,
              svg: { opacity: 1 },
            },
          }}
        >
          <div
            css={{
              width: '100%',
              padding: `${spacing.moderate}px ${spacing.normal}px`,
              wordBreak: 'break-word',
              color: colors.structure.zinc,
            }}
          >
            {content}
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
