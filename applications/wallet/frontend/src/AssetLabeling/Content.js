import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'

import AssetLabelingAdd from './Add'
import AssetLabelingList from './List'

const AssetLabelingContent = ({ projectId, assetId, query }) => {
  const [reloadKey, setReloadKey] = useState(0)

  const triggerReload = () => {
    setReloadKey(reloadKey + 1)
  }

  const {
    data: { results: models },
  } = useSWR(`/api/v1/projects/${projectId}/models/`)

  const {
    data: {
      metadata: {
        source: { filename },
        labels = [],
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  return (
    <>
      <div
        css={{
          padding: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
          color: colors.signal.sky.base,
        }}
      >
        {filename}
      </div>
      <Accordion
        variant={ACCORDION_VARIANTS.PANEL}
        title={
          <span css={{ display: 'flex' }}>
            Select a model and add a label.{' '}
            <Button
              variant={BUTTON_VARIANTS.LINK}
              href={`/${projectId}/models/add`}
              onClick={(event) => event.stopPropagation()}
              css={{
                paddingTop: 0,
                paddingBottom: 0,
                fontWeight: typography.weight.medium,
              }}
            >
              Create New Model
            </Button>
          </span>
        }
        cacheKey="AssetLabeling.Add"
        isInitiallyOpen
        isResizeable={false}
      >
        <AssetLabelingAdd
          key={reloadKey}
          projectId={projectId}
          assetId={assetId}
          models={models}
        />
      </Accordion>

      <Accordion
        variant={ACCORDION_VARIANTS.PANEL}
        title={`Asset Labels: ${labels.length}`}
        cacheKey="AssetLabelingList"
        isInitiallyOpen={false}
        isResizeable={false}
      >
        <AssetLabelingList
          models={models}
          labels={labels}
          projectId={projectId}
          assetId={assetId}
          triggerReload={triggerReload}
          query={query}
        />
      </Accordion>
    </>
  )
}

AssetLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
}

export default AssetLabelingContent
