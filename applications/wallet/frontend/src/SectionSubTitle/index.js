import PropTypes from 'prop-types'

import { spacing, colors } from '../Styles'

const SectionSubTitle = ({ children }) => {
  return (
    <p
      css={{
        margin: 0,
        paddingTop: spacing.base,
        paddingBottom: spacing.normal,
        color: colors.structure.zinc,
      }}>
      {children}
    </p>
  )
}

SectionSubTitle.propTypes = {
  children: PropTypes.node.isRequired,
}

export default SectionSubTitle
