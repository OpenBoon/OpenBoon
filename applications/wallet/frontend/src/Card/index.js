import PropTypes from 'prop-types'

import { spacing, colors, constants } from '../Styles'

const Card = ({ header, children }) => {
  const Element = typeof header === 'string' ? 'h3' : 'div'

  return (
    <div
      css={{
        paddingRight: spacing.spacious,
        paddingBottom: spacing.spacious,
        width: constants.form.maxWidth,
      }}
    >
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          backgroundColor: colors.structure.smoke,
          boxShadow: constants.boxShadows.tableRow,
          borderRadius: constants.borderRadius.small,
        }}
      >
        {!!header && (
          <Element
            css={{
              padding: spacing.normal,
              borderBottom: constants.borders.tabs,
              display: 'flex',
              alignItems: 'center',
              svg: {
                marginRight: spacing.base,
              },
            }}
          >
            {header}
          </Element>
        )}

        {children}
      </div>
    </div>
  )
}

Card.propTypes = {
  header: PropTypes.node.isRequired,
  children: PropTypes.node.isRequired,
}

export default Card
