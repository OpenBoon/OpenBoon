import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

const PageTitle = ({ children }) => {
  return (
    <h2
      css={{
        fontSize: typography.size.mega,
        lineHeight: typography.height.mega,
        fontWeight: typography.weight.regular,
        paddingBottom: spacing.normal,
      }}>
      {children}
    </h2>
  )
}

PageTitle.propTypes = {
  children: PropTypes.string.isRequired,
}

export default PageTitle
