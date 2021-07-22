import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const WebhooksMenu = ({ projectId, webhook, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={async () => {
                    onClick()

                    await fetcher(
                      `/api/v1/projects/${projectId}/webhooks/${webhook.id}/`,
                      {
                        method: 'PUT',
                        body: JSON.stringify({
                          ...webhook,
                          active: !webhook.active,
                        }),
                      },
                    )

                    await revalidate()
                  }}
                >
                  {webhook.active ? 'Deactivate' : 'Activate'}
                </Button>
              </li>

              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()
                    setDeleteModalOpen(true)
                  }}
                >
                  Delete
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>

      {isDeleteModalOpen && (
        <Modal
          title="Delete Webhook"
          message={`Are you sure you want to delete the "${webhook.url}" webhook? This cannot be undone.`}
          action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
          onCancel={() => {
            setDeleteModalOpen(false)
          }}
          onConfirm={async () => {
            setIsDeleting(true)

            await fetcher(
              `/api/v1/projects/${projectId}/webhooks/${webhook.id}/`,
              { method: 'DELETE' },
            )

            await revalidate()

            Router.push(
              '/[projectId]/webhooks?action=delete-webhook-success',
              `/${projectId}/webhooks`,
            )
          }}
        />
      )}
    </>
  )
}

WebhooksMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  webhook: PropTypes.shape({
    id: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
    triggers: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    active: PropTypes.bool.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default WebhooksMenu
