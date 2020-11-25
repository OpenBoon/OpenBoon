import { useState } from 'react'
import Router, { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants, spacing, typography } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import CheckmarkSvg from '../Icons/checkmark.svg'
import FilterSvg from '../Icons/filter.svg'
import PenSvg from '../Icons/pen.svg'

import { encode } from '../Filters/helpers'
import { fetcher, revalidate } from '../Fetch/helpers'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Modal from '../Modal'
import Tabs from '../Tabs'
import ModelLabels from '../ModelLabels'
import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'

import { onTrain } from './helpers'

const LINE_HEIGHT = '23px'

const ModelDetails = () => {
  const {
    query: { projectId, modelId, edit = '' },
  } = useRouter()

  const [error, setError] = useState('')

  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const [, setPanel] = useLocalStorage({
    key: 'leftOpeningPanel',
  })

  const [, setModelFields] = useLocalStorage({
    key: `AssetLabelingAdd.${projectId}`,
    reducer: (state, action) => ({ ...state, ...action }),
    initialState: {
      modelId,
      label: '',
      scope: '',
    },
  })

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
    {
      refreshInterval: 3000,
    },
  )

  const {
    name,
    type,
    unappliedChanges,
    moduleName,
    runningJobId,
    modelTypeRestrictions: {
      requiredLabels,
      missingLabels,
      requiredAssetsPerLabel,
      missingLabelsOnAssets,
    },
  } = model

  const encodedFilter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${moduleName}`,
        modelId,
        values: {},
      },
    ],
  })

  return (
    <>
      {runningJobId && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
            &quot;{name}&quot; training in progress.{' '}
            <Link
              href="/[projectId]/jobs/[jobId]"
              as={`/${projectId}/jobs/${runningJobId}`}
              passHref
            >
              <a>Check Status</a>
            </Link>
          </FlashMessage>
        </div>
      )}

      {error && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>{error}</FlashMessage>
        </div>
      )}

      <ul
        css={{
          margin: 0,
          padding: 0,
          listStyle: 'none',
          fontSize: typography.size.medium,
          lineHeight: LINE_HEIGHT,
        }}
      >
        <li>
          <strong>Model Name:</strong> {name}
        </li>
        <li>
          <strong>Model Type:</strong> {type}
        </li>
        <li>
          <strong>Module Name:</strong> {moduleName}
        </li>
      </ul>

      <div
        css={{
          paddingTop: spacing.base,
          fontStyle: typography.style.italic,
          display: 'flex',
          flexDirection: 'column',
          span: {
            paddingTop: spacing.small,
          },
        }}
      >
        {!!missingLabels && (
          <span
            css={{
              color: colors.signal.canary.base,
            }}
          >
            {missingLabels} more{' '}
            {missingLabels === 1 ? 'label is' : 'labels are'} required (min. ={' '}
            {requiredLabels} unique)
          </span>
        )}

        {!!missingLabelsOnAssets && (
          <span
            css={{
              color: colors.signal.canary.base,
            }}
          >
            {missingLabelsOnAssets} more assets need to be labeled (min. ={' '}
            {requiredAssetsPerLabel} of each label)
          </span>
        )}

        {!missingLabels && !missingLabelsOnAssets && (
          <span
            css={{
              display: 'flex',
              color: colors.signal.grass.base,
            }}
          >
            <CheckmarkSvg
              height={constants.icons.regular}
              color={colors.signal.grass.base}
            />{' '}
            Ready to train
          </span>
        )}
      </div>

      <ButtonGroup>
        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() =>
            onTrain({ model, deploy: false, projectId, modelId, setError })
          }
          isDisabled={
            !unappliedChanges || !!missingLabels || !!missingLabelsOnAssets
          }
        >
          Train
        </Button>

        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() =>
            onTrain({ model, deploy: true, projectId, modelId, setError })
          }
          isDisabled={
            !unappliedChanges || !!missingLabels || !!missingLabelsOnAssets
          }
        >
          Train &amp; Apply
        </Button>

        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() => {
            setDeleteModalOpen(true)
          }}
        >
          Delete
        </Button>

        {isDeleteModalOpen && (
          <Modal
            title="Delete Model"
            message="Deleting this model cannot be undone."
            action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
            onCancel={() => {
              setDeleteModalOpen(false)
            }}
            onConfirm={async () => {
              setIsDeleting(true)

              await fetcher(
                `/api/v1/projects/${projectId}/models/${modelId}/`,
                { method: 'DELETE' },
              )

              await revalidate({
                key: `/api/v1/projects/${projectId}/models/`,
                paginated: true,
              })

              await revalidate({
                key: `/api/v1/projects/${projectId}/models/all/`,
                paginated: false,
              })

              Router.push(
                '/[projectId]/models?action=delete-model-success',
                `/${projectId}/models`,
              )
            }}
          />
        )}
      </ButtonGroup>

      <Tabs
        tabs={[
          {
            title: 'View Labels',
            href: '/[projectId]/models/[modelId]',
            isSelected: edit ? false : undefined,
          },
          edit
            ? {
                title: 'Edit Label',
                href: '/[projectId]/models/[modelId]',
                isSelected: true,
              }
            : {},
        ]}
      />

      {!edit && (
        <div
          css={{
            display: 'flex',
            justifyContent: 'flex-end',
            marginBottom: -spacing.normal,
          }}
        >
          <Link
            href={`/[projectId]/visualizer?query=${encodedFilter}`}
            as={`/${projectId}/visualizer?query=${encodedFilter}`}
            passHref
          >
            <Button
              aria-label="Add Filter in Visualizer"
              variant={BUTTON_VARIANTS.SECONDARY_SMALL}
              onClick={() => {
                localStorage.setItem('rightOpeningPanel', '"filters"')
              }}
              style={{
                display: 'flex',
                paddingTop: spacing.moderate,
                paddingBottom: spacing.moderate,
              }}
            >
              <div css={{ display: 'flex', alignItems: 'center' }}>
                <FilterSvg
                  height={constants.icons.regular}
                  css={{ paddingRight: spacing.base }}
                />
                Add Filter in Visualizer
              </div>
            </Button>
          </Link>

          <div css={{ width: spacing.normal }} />

          <Link
            href="/[projectId]/visualizer"
            as={`/${projectId}/visualizer`}
            passHref
          >
            <Button
              aria-label="Add More Labels"
              variant={BUTTON_VARIANTS.SECONDARY_SMALL}
              onClick={() => {
                setPanel({ value: 'assetLabeling' })

                setModelFields({
                  modelId,
                  scope: SCOPE_OPTIONS[0].value,
                  label: '',
                })
              }}
              style={{
                display: 'flex',
                paddingTop: spacing.moderate,
                paddingBottom: spacing.moderate,
              }}
            >
              <div css={{ display: 'flex', alignItems: 'center' }}>
                <PenSvg
                  height={constants.icons.regular}
                  css={{ paddingRight: spacing.base }}
                />
                Add More Labels
              </div>
            </Button>
          </Link>
        </div>
      )}

      {!edit && <ModelLabels requiredAssetsPerLabel={requiredAssetsPerLabel} />}
    </>
  )
}

export default ModelDetails
