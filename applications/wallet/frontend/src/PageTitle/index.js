import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

const PageTitle = ({ children }) => {
  return (
    <h2
      css={{
        fontSize: typography.size.large,
        lineHeight: typography.height.large,
        fontWeight: typography.weight.regular,
        paddingTop: spacing.comfy,
        paddingBottom: spacing.normal,
      }}
    >
      {children}
    </h2>
  )
}

PageTitle.propTypes = {
  children: PropTypes.string.isRequired,
}

export default PageTitle
