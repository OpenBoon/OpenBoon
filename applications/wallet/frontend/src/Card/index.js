import PropTypes from 'prop-types'

import {colors, constants, spacing, typography} from '../Styles'

const STYLES = {
  DARK: {
    spacer: {
      height: '100%',
      overflow: 'hidden',
      boxShadow: constants.boxShadows.tableRow,
    },
    container: {
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      backgroundColor: colors.structure.lead,
      borderRadius: constants.borderRadius.small,
      padding: spacing.normal,
      paddingTop: spacing.base,
    },
    header: {
      fontSize: typography.size.medium,
      lineHeight: typography.height.medium,
      fontWeight: typography.weight.medium,
      paddingBottom: spacing.comfy,
    },
    content: {
      height: '0%',
      flex: 1,
    },
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Card = ({ variant, header, content }) => {
  const Element = typeof header === 'string' ? 'h3' : 'div'

  return (
    <div css={STYLES[variant].spacer}>
      <div css={STYLES[variant].container}>
        {!!header && <Element css={STYLES[variant].header}>{header}</Element>}

        {!!content && <div css={STYLES[variant].content}>{content}</div>}
      </div>
    </div>
  )
}

Card.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  header: PropTypes.node.isRequired,
  content: PropTypes.node.isRequired,
}

export default Card
