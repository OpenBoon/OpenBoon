import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const IMG_HEIGHT = 32

const Providers = ({ providers, initialModules, modules, dispatch }) => {
  return providers.map(({ name, logo, description, categories }) => {
    return (
      <div key={name} css={{ paddingTop: spacing.normal }}>
        <Accordion
          variant={ACCORDION_VARIANTS.PRIMARY}
          icon={<img src={logo} alt={name} height={IMG_HEIGHT} />}
          title={name}
          hideTitle
          cacheKey={`ProvidersProvider.${name}`}
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
                  options: category.modules.map((module) => ({
                    value: module.name,
                    label: module.description,
                    initialValue: (modules && !!modules[module.name]) || false,
                    isDisabled:
                      (initialModules && !!initialModules[module.name]) ||
                      false,
                    supportedMedia: module.supportedMedia,
                  })),
                }}
                onClick={(module) => {
                  dispatch({ modules: { ...modules, ...module } })
                }}
              />
            ))}
          </>
        </Accordion>
      </div>
    )
  })
}

Providers.propTypes = {
  providers: PropTypes.arrayOf(
    PropTypes.shape({
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
  ).isRequired,
  initialModules: PropTypes.shape({}).isRequired,
  modules: PropTypes.shape({}).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default Providers
