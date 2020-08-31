import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const ApiKeysMenu = ({ projectId, apiKeyId, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  return (
    <Menu open="left" button={ButtonActions}>
      {({ onClick }) => (
        <div>
          <ul>
            <li>
              <>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    setDeleteModalOpen(true)
                  }}
                  isDisabled={false}
                >
                  Delete
                </Button>
                {isDeleteModalOpen && (
                  <Modal
                    title="Delete API Key"
                    message="Deleting this key cannot be undone."
                    action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
                    onCancel={() => {
                      setDeleteModalOpen(false)

                      onClick()
                    }}
                    onConfirm={async () => {
                      setIsDeleting(true)

                      await fetcher(
                        `/api/v1/projects/${projectId}/api_keys/${apiKeyId}/`,
                        { method: 'DELETE' },
                      )

                      await revalidate()

                      Router.push(
                        '/[projectId]/api-keys?action=delete-apikey-success',
                        `/${projectId}/api-keys`,
                      )
                    }}
                  />
                )}
              </>
            </li>
          </ul>
        </div>
      )}
    </Menu>
  )
}

ApiKeysMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  apiKeyId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ApiKeysMenu
