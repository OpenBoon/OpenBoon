import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const IMG_HEIGHT = 32

const Providers = ({ providers, modules, fileTypes, dispatch }) => {
  const providersFilterModules = providers.map((p) => ({
    ...p,
    categories: p.categories.map((c) => ({
      ...c,
      modules: c.modules.filter((m) => {
        const intersection = m.supportedMedia.filter((sM) =>
          fileTypes.includes(sM),
        )
        return intersection.length > 0
      }),
    })),
  }))

  const providersFilterCategories = providersFilterModules.map((p) => ({
    ...p,
    categories: p.categories.filter((c) => c.modules.length > 0),
  }))

  const providersFilterProviders = providersFilterCategories.filter(
    (p) => p.categories.length > 0,
  )

  return providersFilterProviders.map(
    ({ name, logo, description, categories }) => {
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
                      initialValue:
                        (modules && !!modules[module.name]) || false,
                      isDisabled: false,
                      supportedMedia: module.supportedMedia,
                    })),
                  }}
                  onClick={(module) => {
                    console.log(module)
                    dispatch({ modules: { ...modules, ...module } })
                  }}
                />
              ))}
            </>
          </Accordion>
        </div>
      )
    },
  )
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
          modules: PropTypes.arrayOf(
            PropTypes.shape({
              id: PropTypes.string.isRequired,
              name: PropTypes.string.isRequired,
              description: PropTypes.string.isRequired,
              provider: PropTypes.string.isRequired,
              category: PropTypes.string.isRequired,
              supportedMedia: PropTypes.arrayOf(
                PropTypes.oneOf(['Images', 'Documents', 'Videos']).isRequired,
              ).isRequired,
            }),
          ).isRequired,
        }).isRequired,
      ).isRequired,
    }).isRequired,
  ).isRequired,
  modules: PropTypes.shape({}).isRequired,
  fileTypes: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default Providers
