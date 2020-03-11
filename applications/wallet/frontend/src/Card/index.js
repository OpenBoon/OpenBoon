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
        }}>
        {!!title && (
          <h3
            css={{
              padding: spacing.normal,
              borderBottom: constants.borders.tabs,
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
  title: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default Card
