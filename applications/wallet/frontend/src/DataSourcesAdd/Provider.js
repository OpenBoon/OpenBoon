import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Accordion, { VARIANTS } from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const IMG_HEIGHT = 32

const DataSourcesAddProvider = ({
  provider: { name, logo, description, categories },
  onClick,
}) => {
  return (
    <div css={{ paddingTop: spacing.normal }}>
      <Accordion
        variant={VARIANTS.PRIMARY}
        title={<img src={logo} alt={name} height={IMG_HEIGHT} />}
        cacheKey={`DataSourcesAddProvider.${name}`}
        isInitiallyOpen
      >
        <>
          <p
            css={{
              color: colors.structure.zinc,
              margin: 0,
              paddingTop: spacing.base,
              paddingBottom: spacing.normal,
              maxWidth: constants.paragraph.maxWidth,
            }}
          >
            {description}
          </p>
          {categories.map((category) => (
            <CheckboxTable
              key={category.name}
              category={{
                name: category.name,
                options: category.modules.map((module) => ({
                  value: module.name,
                  label: module.description,
                  initialValue: false,
                  isDisabled: module.restricted,
                })),
              }}
              onClick={onClick}
            />
          ))}
        </>
      </Accordion>
    </div>
  )
}

DataSourcesAddProvider.propTypes = {
  provider: PropTypes.shape({
    name: PropTypes.string.isRequired,
    logo: PropTypes.node.isRequired,
    description: PropTypes.node.isRequired,
    categories: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        modules: PropTypes.arrayOf(PropTypes.shape()).isRequired,
      }).isRequired,
    ).isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesAddProvider
