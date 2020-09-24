import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

import AssetLabelingAdd from './Add'
import AssetLabelingList from './List'

const AssetLabelingContent = ({ projectId, assetId }) => {
  const [reloadKey, setReloadKey] = useState(0)
  const [error, setError] = useState('')

  const triggerReload = () => {
    setReloadKey(reloadKey + 1)
  }

  const {
    data: { results: models },
  } = useSWR(`/api/v1/projects/${projectId}/models/all/`)

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
        title="Select a model and add a label"
        actions={
          <Button
            variant={BUTTON_VARIANTS.LINK}
            href={`/${projectId}/models/add`}
            onClick={(event) => event.stopPropagation()}
            css={{
              paddingTop: 0,
              paddingBottom: 0,
              marginTop: -spacing.mini,
              fontWeight: typography.weight.medium,
            }}
          >
            Create New Model
          </Button>
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
          labels={labels}
        />
      </Accordion>

      <Accordion
        variant={ACCORDION_VARIANTS.PANEL}
        title={`Asset Labels: ${labels.length}`}
        cacheKey="AssetLabelingList"
        isInitiallyOpen={false}
        isResizeable={false}
      >
        <>
          {error && (
            <div css={{ padding: spacing.normal }}>
              <FlashMessage variant={FLASH_VARIANTS.ERROR}>
                {error}
              </FlashMessage>
            </div>
          )}
          <AssetLabelingList
            models={models}
            labels={labels}
            triggerReload={triggerReload}
            setError={setError}
          />
        </>
      </Accordion>
    </>
  )
}

AssetLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default AssetLabelingContent
