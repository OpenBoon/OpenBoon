import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const IMG_HEIGHT = 32

const DataSourcesEditProvider = ({
  provider: { name, logo, description, categories },
  initialModules,
  modules,
  onClick,
}) => {
  return (
    <div css={{ paddingTop: spacing.normal }}>
      <Accordion
        variant={ACCORDION_VARIANTS.PRIMARY}
        icon={<img src={logo} alt={name} height={IMG_HEIGHT} />}
        title={name}
        hideTitle
        cacheKey={`DataSourcesEditProvider.${name}`}
        isInitiallyOpen
        isResizeable={false}
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
                options: category.modules.map((module) => {
                  return {
                    value: module.name,
                    label: module.description,
                    initialValue: !!modules[module.name],
                    isDisabled: !!initialModules[module.name],
                    supportedMedia: module.supportedMedia,
                  }
                }),
              }}
              onClick={onClick}
            />
          ))}
        </>
      </Accordion>
    </div>
  )
}

DataSourcesEditProvider.propTypes = {
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
  initialModules: PropTypes.shape({}).isRequired,
  modules: PropTypes.shape({}).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesEditProvider
