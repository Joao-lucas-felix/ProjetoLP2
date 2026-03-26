import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  private readonly http = inject(HttpClient);
  private readonly endpoint = 'http://localhost:8080/extract-palette';

  selectedFile: File | null = null;
  originalImageUrl: string | null = null;
  paletteImageUrl: string | null = null;
  isLoading = false;
  isDragging = false;
  errorMessage = '';
  statusMessage = 'Arraste uma imagem PNG ou JPEG para a forja ou selecione um arquivo.';

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.applySelectedFile(file);
  }

  extractPalette(): void {
    if (!this.selectedFile) {
      this.errorMessage = 'Escolha uma imagem antes de extrair a paleta.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.statusMessage = 'Enviando imagem para o endpoint /extract-palette...';

    this.http.post(this.endpoint, this.selectedFile, { responseType: 'blob' }).subscribe({
      next: (paletteBlob) => {
        this.paletteImageUrl = URL.createObjectURL(paletteBlob);
        this.statusMessage = 'Paleta recebida com sucesso.';
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = this.buildErrorMessage(error);
        this.statusMessage = 'Falha ao gerar a paleta.';
      }
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    const file = event.dataTransfer?.files?.[0] ?? null;
    this.applySelectedFile(file);
  }

  private resetResults(): void {
    this.errorMessage = '';
    if (this.paletteImageUrl) {
      URL.revokeObjectURL(this.paletteImageUrl);
    }
    this.paletteImageUrl = null;
  }

  private applySelectedFile(file: File | null): void {
    this.resetResults();
    if (this.originalImageUrl) {
      URL.revokeObjectURL(this.originalImageUrl);
      this.originalImageUrl = null;
    }

    this.selectedFile = file;

    if (!file) {
      this.statusMessage = 'Nenhum arquivo selecionado.';
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.selectedFile = null;
      this.errorMessage = 'Selecione um arquivo de imagem valido.';
      this.statusMessage = 'O servidor aceita PNG ou JPEG.';
      return;
    }

    this.originalImageUrl = URL.createObjectURL(file);
    this.statusMessage = `Arquivo pronto para envio: ${file.name}`;
  }

  private buildErrorMessage(error: HttpErrorResponse): string {
    if (typeof error.error === 'string' && error.error.length > 0) {
      return error.error;
    }

    if (error.error instanceof Blob) {
      return `Erro HTTP ${error.status}: o servidor retornou um corpo nao textual.`;
    }

    return `Erro HTTP ${error.status || 0}: ${error.message}`;
  }
}
