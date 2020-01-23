import PropTypes from 'prop-types'

import { spacing, typography, colors } from '../Styles'

const Breadcrumbs = ({ crumbs }) => {
  return (
    <div css={{ display: 'flex' }}>
      {crumbs.map((crumb, index) => {
        const isLastCrumb = index === crumbs.length - 1
        const content = !isLastCrumb ? `${crumb} /` : `${crumb}`
        return (
          <div
            key={crumb}
            css={{
              fontSize: typography.size.large,
              lineHeight: typography.height.large,
              fontWeight: typography.weight.regular,
              paddingTop: spacing.comfy,
              paddingBottom: spacing.normal,
              paddingRight: isLastCrumb ? 0 : spacing.small,
              color: isLastCrumb
                ? colors.structure.white
                : colors.structure.steel,
            }}>
            {content}
          </div>
        )
      })}
    </div>
  )
}

Breadcrumbs.propTypes = {
  crumbs: PropTypes.arrayOf(PropTypes.string).isRequired,
}

export default Breadcrumbs
