import PropTypes from 'prop-types'

import { typography, colors, spacing, constants } from '../Styles'

export const CHECKMARK_WIDTH = 28

const Accordion = ({ title, children }) => {
  return (
    <div css={{ paddingTop: spacing.normal }}>
      <div
        css={{
          backgroundColor: colors.structure.smoke,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
        }}
      >
        <div
          css={{
            borderBottom: constants.borders.tabs,
            padding: spacing.normal,
          }}
        >
          <h4
            css={{
              fontSize: typography.size.medium,
              lineHeight: typography.height.medium,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {title}
          </h4>
        </div>
        <div
          css={{
            padding: spacing.spacious,
            paddingTop: spacing.normal,
          }}
        >
          {children}
        </div>
      </div>
    </div>
  )
}

Accordion.propTypes = {
  title: PropTypes.node.isRequired,
  children: PropTypes.node.isRequired,
}

export default Accordion
