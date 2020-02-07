import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

const SectionTitle = ({ children }) => {
  return (
    <h3
      css={{
        fontSize: typography.size.medium,
        lineHeight: typography.height.medium,
        fontWeight: typography.weight.medium,
        paddingTop: spacing.normal,
      }}>
      {children}
    </h3>
  )
}

SectionTitle.propTypes = {
  children: PropTypes.node.isRequired,
}

export default SectionTitle
