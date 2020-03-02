import PropTypes from 'prop-types'

import checkboxOptionShape from '../Checkbox/optionShape'

import { colors, spacing, constants } from '../Styles'

import Accordion from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const DataSourcesAddModules = ({
  module: { logo, description, categories },
  onClick,
}) => {
  return (
    <Accordion title={logo}>
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
    logo: PropTypes.node.isRequired,
    description: PropTypes.node.isRequired,
    categories: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        options: PropTypes.arrayOf(PropTypes.shape(checkboxOptionShape))
          .isRequired,
      }).isRequired,
    ).isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesAddModules
