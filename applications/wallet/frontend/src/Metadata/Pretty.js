import assetShape from '../Asset/shape'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'

import MetadataAnalysis from './Analysis'
import MetadataPrettyRow from './PrettyRow'
import { formatDisplayName } from './helpers'

const MetadataPretty = ({ asset: { metadata } }) => {
  return (
    <div css={{ overflow: 'auto' }}>
      {Object.keys(metadata).map((section) => {
        const title = formatDisplayName({ name: section })

        if (['files', 'metrics', 'location'].includes(section)) return null

        return (
          <Accordion
            key={section}
            variant={ACCORDION_VARIANTS.PANEL}
            title={title}
            isInitiallyOpen
          >
            {section === 'analysis' ? (
              <MetadataAnalysis />
            ) : (
              <table
                css={{
                  borderCollapse: 'collapse',
                  width: '100%',
                }}
              >
                <tbody>
                  {Object.entries(metadata[section]).map(
                    ([key, value], index) => (
                      <MetadataPrettyRow
                        key={key}
                        name={key}
                        value={value}
                        title={title}
                        index={index}
                      />
                    ),
                  )}
                </tbody>
              </table>
            )}
          </Accordion>
        )
      })}
    </div>
  )
}

MetadataPretty.propTypes = {
  asset: assetShape.isRequired,
}

export default MetadataPretty
