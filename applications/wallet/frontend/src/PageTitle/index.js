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
        display: 'flex',
        alignItems: 'center',
      }}
    >
      {children}
    </h2>
  )
}

PageTitle.propTypes = {
  children: PropTypes.node.isRequired,
}

export default PageTitle
