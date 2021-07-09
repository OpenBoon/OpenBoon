import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'

import { spacing, constants } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import ItemTitle from '../Item/Title'
import ItemList from '../Item/List'
import ItemSeparator from '../Item/Separator'
import Menu from '../Menu'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { formatFullDate } from '../Date/helpers'

import KebabSvg from '../Icons/kebab.svg'

import { onTrain } from './helpers'

import ModelMatrixLink from './MatrixLink'
import ModelTip from './Tip'
import ModelDeleteModal from './DeleteModal'

const ModelDetails = ({ projectId, modelId, modelTypes }) => {
  const [error, setError] = useState('')
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
    {
      refreshInterval: 3000,
    },
  )

  const {
    name,
    type,
    description,
    runningJobId,
    modelTypeRestrictions: { missingLabels },
    timeLastTrained,
    timeLastApplied,
  } = model

  const { label } = modelTypes.find(({ name: n }) => n === type) || {
    label: type,
  }

  return (
    <div>
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

      <div>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
          }}
        >
          <ItemTitle type="Model" name={name} />

          <Menu
            open="bottom-left"
            button={({ onBlur, onClick }) => (
              <Button
                aria-label="Toggle Actions Menu"
                variant={BUTTON_VARIANTS.SECONDARY}
                onBlur={onBlur}
                onClick={onClick}
                style={{
                  padding: spacing.moderate,
                  marginBottom: spacing.small,
                }}
              >
                <KebabSvg height={constants.icons.regular} />
              </Button>
            )}
          >
            {({ onBlur, onClick }) => (
              <div>
                <ul>
                  <li>
                    <Button
                      variant={BUTTON_VARIANTS.MENU_ITEM}
                      onBlur={onBlur}
                      onClick={async () => {
                        onClick()
                        setDeleteModalOpen(true)
                      }}
                    >
                      Delete Model
                    </Button>
                  </li>
                </ul>
              </div>
            )}
          </Menu>

          <ModelDeleteModal
            projectId={projectId}
            modelId={modelId}
            isDeleteModalOpen={isDeleteModalOpen}
            setDeleteModalOpen={setDeleteModalOpen}
          />
        </div>

        <ItemList
          attributes={[
            ['Model Type', label],
            ['Description', description],
          ]}
        />
      </div>

      <div css={{ height: spacing.normal }} />

      <ItemSeparator />

      <div css={{ height: spacing.normal }} />

      <div css={{ display: 'flex', justifyContent: 'space-between' }}>
        <div>
          <ItemList
            attributes={[
              [
                'Last Trained',
                timeLastTrained
                  ? formatFullDate({ timestamp: timeLastTrained })
                  : 'Untrained',
              ],
              [
                'Last Analyzed',
                timeLastApplied
                  ? formatFullDate({ timestamp: timeLastApplied })
                  : 'Model Analysis has not been run.',
              ],
            ]}
          />

          <ButtonGroup>
            <Button
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() =>
                onTrain({
                  model,
                  apply: false,
                  test: false,
                  projectId,
                  modelId,
                  setError,
                })
              }
              isDisabled={!!missingLabels}
            >
              Train Model
            </Button>

            <Button
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() =>
                onTrain({
                  model,
                  apply: false,
                  test: true,
                  projectId,
                  modelId,
                  setError,
                })
              }
              isDisabled={!!missingLabels}
            >
              Train &amp; Test
            </Button>

            <Button
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() =>
                onTrain({
                  model,
                  apply: true,
                  test: false,
                  projectId,
                  modelId,
                  setError,
                })
              }
              isDisabled={!!missingLabels}
            >
              Train &amp; Analyze All
            </Button>

            <ModelTip />
          </ButtonGroup>
        </div>

        <ModelMatrixLink projectId={projectId} model={model} />
      </div>
    </div>
  )
}

ModelDetails.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  modelTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default ModelDetails
