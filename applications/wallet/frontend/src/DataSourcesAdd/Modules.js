import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Accordion from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const DataSourcesAddModules = ({
  module: { logoSvg, description, categories },
  onClick,
}) => {
  return (
    <Accordion title={logoSvg}>
      <>
        <p
          css={{
            color: colors.structure.zinc,
            margin: 0,
            paddingTop: spacing.base,
            paddingBottom: spacing.normal,
            maxWidth: constants.paragraph.maxWidth,
          }}>
          {description}
        </p>
        {categories.map(category => (
          <CheckboxTable
            key={category.name}
            category={category}
            onClick={onClick}
          />
        ))}
      </>
    </Accordion>
  )
}

DataSourcesAddModules.propTypes = {
  module: PropTypes.shape({
    provider: PropTypes.string.isRequired,
    logoSvg: PropTypes.node.isRequired,
    description: PropTypes.node.isRequired,
    categories: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        modules: PropTypes.arrayOf(
          PropTypes.shape({
            key: PropTypes.string.isRequired,
            label: PropTypes.string.isRequired,
            legend: PropTypes.string.isRequired,
          }).isRequired,
        ).isRequired,
      }).isRequired,
    ).isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesAddModules
