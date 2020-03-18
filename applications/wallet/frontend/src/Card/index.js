import PropTypes from 'prop-types'

import { spacing, colors, constants } from '../Styles'

const Card = ({ title, children }) => {
  return (
    <div
      css={{
        paddingRight: spacing.spacious,
        paddingBottom: spacing.spacious,
        width: constants.form.maxWidth,
      }}>
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          backgroundColor: colors.structure.smoke,
          boxShadow: constants.boxShadows.tableRow,
          borderRadius: constants.borderRadius.small,
        }}>
        {!!title && (
          <h3
            css={{
              padding: spacing.normal,
              borderBottom: constants.borders.tabs,
              display: 'flex',
              alignItems: 'center',
              svg: {
                marginRight: spacing.base,
              },
            }}>
            {title}
          </h3>
        )}
        <div css={{ padding: spacing.spacious }}>{children}</div>
      </div>
    </div>
  )
}

Card.propTypes = {
  title: PropTypes.node.isRequired,
  children: PropTypes.node.isRequired,
}

export default Card
