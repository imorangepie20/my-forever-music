from __future__ import annotations

import torch
from torch import nn


class SasrecMvpModel(nn.Module):
    def __init__(
        self,
        vocabulary_size: int,
        max_context_length: int,
        hidden_size: int,
        attention_heads: int,
        dropout: float = 0.1,
    ) -> None:
        super().__init__()
        self.item_embedding = nn.Embedding(
            num_embeddings=vocabulary_size + 1,
            embedding_dim=hidden_size,
            padding_idx=0,
        )
        self.position_embedding = nn.Embedding(
            num_embeddings=max_context_length,
            embedding_dim=hidden_size,
        )
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=hidden_size,
            nhead=attention_heads,
            dim_feedforward=hidden_size * 4,
            dropout=dropout,
            batch_first=True,
            activation="gelu",
        )
        self.encoder = nn.TransformerEncoder(encoder_layer, num_layers=1)
        self.output = nn.Linear(hidden_size, vocabulary_size + 1)

    def forward(self, context_item_indices: torch.Tensor) -> torch.Tensor:
        sequence_length = context_item_indices.shape[1]
        positions = torch.arange(
            sequence_length,
            device=context_item_indices.device,
        ).unsqueeze(0)
        embeddings = self.item_embedding(context_item_indices) + self.position_embedding(positions)
        padding_mask = context_item_indices.eq(0)
        encoded = self.encoder(embeddings, src_key_padding_mask=padding_mask)
        last_positions = context_item_indices.ne(0).sum(dim=1).clamp(min=1) - 1
        batch_indices = torch.arange(context_item_indices.shape[0], device=context_item_indices.device)
        final_state = encoded[batch_indices, last_positions]
        return self.output(final_state)
